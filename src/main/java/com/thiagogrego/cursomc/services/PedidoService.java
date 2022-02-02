package com.thiagogrego.cursomc.services;

import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thiagogrego.cursomc.domain.ItemPedido;
import com.thiagogrego.cursomc.domain.PagamentoComBoleto;
import com.thiagogrego.cursomc.domain.Pedido;
import com.thiagogrego.cursomc.domain.enums.EstadoPagamento;
import com.thiagogrego.cursomc.repositories.ItemPedidoRepository;
import com.thiagogrego.cursomc.repositories.PagamentoRepository;
import com.thiagogrego.cursomc.repositories.PedidoRepository;
import com.thiagogrego.cursomc.services.exceptions.ObjectNotFoundException;

@Service
public class PedidoService {

	@Autowired
	private PedidoRepository repo;
	
	@Autowired
	private BoletoService boletoService;
	
	@Autowired
	private PagamentoRepository pagamentoRepository;
	
	@Autowired
	private ItemPedidoRepository itemPedidoRepository;

	@Autowired
	private ProdutoService produtoService;

	public Pedido findById(Integer id) {
		Optional<Pedido> pedido = repo.findById(id);
		
		if(pedido.isEmpty()) {
			return pedido.orElseThrow(() -> new ObjectNotFoundException(
					"Objeto não encontrado! Id: " + id + ", Tipo: " + Pedido.class.getName()));
		}
		
		return pedido.get();
		
	}
	
	@Transactional
	public Pedido save(Pedido pedido) {
		pedido.setId(null);
		pedido.setInstante(new Date());
		pedido.getPagamento().setEstado(EstadoPagamento.PENDENTE);
		pedido.getPagamento().setPedido(pedido);
		if (pedido.getPagamento() instanceof PagamentoComBoleto) {
			PagamentoComBoleto pagto = (PagamentoComBoleto) pedido.getPagamento();
			boletoService.preencherPagamentoComBoleto(pagto, pedido.getInstante());
		}
		pedido = repo.save(pedido);
		pagamentoRepository.save(pedido.getPagamento());
		for (ItemPedido ip : pedido.getItens()) {
			ip.setDesconto(0.0);
			ip.setPreco(produtoService.findById(ip.getProduto().getId()).getPreco());
			ip.setPedido(pedido);
		}
		itemPedidoRepository.saveAll(pedido.getItens());
		return pedido;
	}
}
